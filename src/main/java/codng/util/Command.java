package codng.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility class to launch external programs and capture the output.
 */
public class Command
{
    //~ Instance fields ......................................................................................

    private final List<String>        command;
    private final Map<String, String> environment;
    private OutputStream              stderr = NullOutputStream.INSTANCE;
    private OutputStream              stdout = NullOutputStream.INSTANCE;
    private File                      workdir;
    private String                    input;

    //~ Constructors .........................................................................................

    public Command(final String... command)
    {
        this.command = new ArrayList<>();
        environment = new HashMap<>();
        this.command.addAll(Arrays.asList(command));
    }

    //~ Methods ..............................................................................................

    public Command args(List<String> args)
    {
        if (args == null) {
            throw new IllegalArgumentException("argument is not optional");
        }

        command.addAll(args);
        return this;
    }

    public Command define(final String name, final String value)
    {
        environment.put(name, value);
        return this;
    }

    public Command workdir(final File wd)
    {
        workdir = wd;
        return this;
    }

    public int run()
        throws IOException, InterruptedException
    {
        Process process = runInternal();

        StreamTransfer out = asyncTransfer(process.getInputStream(), stdout);
        StreamTransfer err = asyncTransfer(process.getErrorStream(), stderr);

        if (input != null) {
            OutputStream in = process.getOutputStream();
            in.write(input.getBytes());
            in.flush();
            in.close();
        }

        int exitStaus = process.waitFor();
        out.join();
        err.join();        
        return exitStaus;
    }

    public Callable<Integer> start()
        throws IOException, InterruptedException
    {
        final Process process = runInternal();

        final StreamTransfer out = asyncTransfer(process.getInputStream(), stdout);
        final StreamTransfer err = asyncTransfer(process.getErrorStream(), stderr);

        if (input != null) {
            OutputStream in = process.getOutputStream();
            in.write(input.getBytes());
            in.flush();
            in.close();
        }

		return new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				int exitStaus = process.waitFor();
				out.join();
				err.join();        
				return exitStaus;
			}
		};
    }

    public Command input(String input) {
        this.input = input;
        return this;
    }

    public Command redirectOutput(final OutputStream out)
    {
        if (out == null) {
            stdout = NullOutputStream.INSTANCE;
        }

        stdout = out;
        return this;
    }

    public Command redirectError(final OutputStream err)
    {
        if (err == null) {
            stderr = NullOutputStream.INSTANCE;
        }

        stderr = err;
        return this;
    }

    public Command args(String... args)
    {
        return args(Arrays.asList(args));
    }

    private static StreamTransfer asyncTransfer(InputStream in, OutputStream out)
    {
        StreamTransfer streamTransfer = new StreamTransfer(in, out);
        streamTransfer.start();
        return streamTransfer;
    }

    /**
     * Copies input stream to output stream efficiently. Streams are not closed on return
     *
     * @param inStream input stream.
     * @param outStream output stream.
     * @return Number of bytes copied
     * @throws IOException if an underlying operation fails
     */
    private static long copy(InputStream inStream, OutputStream outStream)
        throws IOException
    {
        final int    buffSize = 8192;
        final byte[] buff = new byte[buffSize];
        long         transferred = 0;

        IOException exception = null;
        int         len = 0;
        int         nread;

        do {
            try {
                nread = inStream.read(buff, len, buffSize - len);
            }
            catch (IOException e) {
                // In case of exception, write buffer first, then re-raise
                exception = e;
                nread = -1;
            }

            // Write buffer at EOF or when at least half buffer is full
            if (nread == -1 ? len > 0 : (len += nread) > buffSize / 2) {
                outStream.write(buff, 0, len);
                transferred += len;
                len = 0;
            }
        }
        while (nread != -1);

        if (exception != null) {
            throw exception;
        }

        return transferred;
    }

    private Process runInternal()
        throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(environment);

        if (workdir != null) {
            builder.directory(workdir);
        }

        return builder.start();
    }

    static List<String> executeSplit(File workDir, String command, List<String> args)
            throws IOException
    {
        return splitLines(executeInternal(workDir, command, args, null, true));
    }

    static List<String> executeSplit(File workDir, String command, List<String> args, boolean printOut)
            throws IOException {
        return splitLines(executeInternal(workDir, command, args, null, printOut));
    }
    
    private static ByteArrayOutputStream executeInternal(File workDir, String command, List<String> args, String input, boolean printOut) throws IOException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try {
            System.out.println("running " + command + " " + args + "\n\t current directory: " + workDir);
            exitCode = new Command(command)
                    .args(args)
                    .redirectOutput(stdout)
                    .redirectError(stderr)
                    .input(input)
                    .workdir(workDir)
                    .run();
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        if (printOut) {
            System.out.println(stdout);
        }

        if(exitCode != 0) {
            throw new IOException(stderr + "\n" + stdout);
        }
        
        return stdout;
    }

    private static ArrayList<String> splitLines(final ByteArrayOutputStream stdout) {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(stdout.toByteArray())));

        final ArrayList<String> result = new ArrayList<String>();

        try {
            for(String line = br.readLine(); line != null; line = br.readLine()) {
                result.add(line);
            }
        } catch (IOException e) {
            //Can't happen
            throw new Error(e);
        }
        return result;
    }

	
    public static String executeSimple(String command, String... args)
            throws IOException
    {
        return executeInternal(new File(".").getAbsoluteFile(), command, Arrays.asList(args), null, true).toString();
    }
	
	public static String executeSimple(File workDir, String command, List<String> args)
            throws IOException
    {
        return executeInternal(workDir, command, args, null, true).toString();
    }

    public static String executeSimple(File workDir, String command, List<String> args, String input)
            throws IOException
    {
        return executeInternal(workDir, command, args, input, true).toString();
    }

    public static byte[] executeBinary(File workDir, String command, List<String> args)
            throws IOException
    {
        return executeInternal(workDir, command, args, null, true).toByteArray();
    }

    public static int executeBinaryWithReturnCode(File workDir, String command, List<String> args)
            throws IOException
    {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try {
            System.out.println("running " + command + " " + args + "\n\t current directory: " + workDir);
            exitCode = new Command(command)
                    .args(args)
                    .redirectOutput(System.out)
                    .redirectError(stderr)
                    .workdir(workDir)
                    .run();
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        return exitCode;
    }

    public static void executeToStdout(final File workDir, String... args) throws IOException {
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.out.println("running: " + Arrays.asList(args));

        int exitCode;
        try {
            exitCode = new Command(args)
                    .redirectOutput(System.out)
                    .redirectError(stderr)
                    .workdir(workDir)
                    .run();
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        if(exitCode != 0) {
            throw new IOException(stderr.toString());
        }
    }

    //~ Inner Classes ........................................................................................

    private static final class NullOutputStream
        extends OutputStream
    {
        public void write(final int b)
        {
            // Do nothing
        }

        @Override public void write(final byte[] b, final int off, final int len)
        {
            assert off >= 0 && len >= 0 && (off + len) <= b.length;
                   // do nothing
        }

        static final OutputStream INSTANCE = new NullOutputStream();
    }

    private static class StreamTransfer
        extends Thread
    {
        private final InputStream  in;
        private final OutputStream out;

        public StreamTransfer(InputStream in, OutputStream out)
        {
            this.in = in;
            this.out = out;
        }

        @Override public void run()
        {
            try {
                copy(in, out);
            }
            catch (IOException e) {
                //I really don't care that much
                e.printStackTrace();
            }
        }
    }
}
