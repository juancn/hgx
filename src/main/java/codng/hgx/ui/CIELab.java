package codng.hgx.ui;

import java.awt.color.ColorSpace;

public class CIELab extends ColorSpace {

	private CIELab() {
		super(ColorSpace.TYPE_Lab, 3);
	}

	public static CIELab getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public float[] fromCIEXYZ(float[] colorvalue) {
		double l = f(colorvalue[1]);
		double L = 116.0 * l - 16.0;
		double a = 500.0 * (f(colorvalue[0]) - l);
		double b = 200.0 * (l - f(colorvalue[2]));
		return new float[] {(float) L, (float) a, (float) b};
	}

	@Override
	public float[] fromRGB(float[] rgbvalue) {
		float[] xyz = CIEXYZ.fromRGB(rgbvalue);
		return fromCIEXYZ(xyz);
	}

	@Override
	public float getMaxValue(int component) {
		return 128f;
	}

	@Override
	public float getMinValue(int component) {
		return (component == 0)? 0f: -128f;
	}

	@Override
	public String getName(int idx) {
		return String.valueOf("Lab".charAt(idx));
	}

	@Override
	public float[] toCIEXYZ(float[] colorvalue) {
		double i = (colorvalue[0] + 16.0) * (1.0 / 116.0);
		double X = fInv(i + colorvalue[1] * (1.0 / 500.0));
		double Y = fInv(i);
		double Z = fInv(i - colorvalue[2] * (1.0 / 200.0));
		return new float[] {(float) X, (float) Y, (float) Z};
	}

	@Override
	public float[] toRGB(float[] colorvalue) {
		return xyzToRGB(toCIEXYZ(colorvalue));
	}

	private float[] xyzToRGB(float[] xyz) {
		float rl =  3.2406f*xyz[0] - 1.5372f*xyz[1] - 0.4986f*xyz[2];
		float gl = -0.9689f*xyz[0] + 1.8758f*xyz[1] + 0.0415f*xyz[2];
		float bl =  0.0557f*xyz[0] - 0.2040f*xyz[1] + 1.0570f*xyz[2];
		return new float[] { linearTosRGB(rl), linearTosRGB(gl), linearTosRGB(bl)};
	}

	private static float linearTosRGB(float c) {
		final float a = 0.055f;
		return clamp(c <= 0.0031308 ? 12.92f*c : (1 + a )*(float)Math.pow(c, 1/2.4f)- a);
	}

	private static float clamp(float v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	private static double f(double x) {
		if (x > 216.0 / 24389.0) {
			return Math.cbrt(x);
		} else {
			return (841.0 / 108.0) * x + N;
		}
	}

	private static double fInv(double x) {
		if (x > 6.0 / 29.0) {
			return x*x*x;
		} else {
			return (108.0 / 841.0) * (x - N);
		}
	}

	private Object readResolve() {
		return getInstance();
	}

	private static class Holder {
		static final CIELab INSTANCE = new CIELab();
	}

	private static final long serialVersionUID = 5027741380892134289L;

	private static final ColorSpace CIEXYZ =
			ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

	private static final double N = 4.0 / 29.0;

}