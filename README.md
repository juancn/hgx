HgX
===

HgX is a Mercurial history viewer inspired by GitX. 

!["JCarder repository screenshot"](hgx/blob/master/doc/jcarder.png?raw=true)

Overview
-------

A few months ago I started a new job at a company that uses Mercurial as its repository. We do development in Java using Macs, and coming from git, the lack of decent (i.e. pretty) history viewers for mercurial bothered me.

I also found that most tools did not handle large diffs gracefully, basically they either died or were dead slow.

One reason for this slowness is Mercurial's lack of an API other than the command line (it has an internal, unsupported one that's used from python).

Since I couldn't find any viewer that I liked, I decided to write my own. HgX is minimalistic, opinionated history viewer. The code is somewhat messy, I try to keep it clean, but results vary.

### Speed 

HgX attempts to solve some of the performance issues with Mercurial through caching and a fast custom renderer for the diffs.

HgX maintains a cache in $HOME/.hgx. This directory can be removed at any time and it will be rebuilt.

At this point syntax highlighting is available only for Java, but it is possible to add more languages.

Build & Usage
-------------

Once you have a working copy, just run:

    mvm package

You can run it by executing:

    hgx

(assumning hgx is in you path).
You can see command line options by running the classic:

    hgx --help


Cveats and Limitations
----------------------

This is tested mainly on (my) Mac, on other OSes, your mileage may vary. This is a Quick and Dirty™ implementation, and is buggy and incomplete.

HgX is developed in Java 7, it should be fairly easy to port to earlier versions (I'm not using much other than the new generic's shorthand notation).

The functionality is heavily restricted in many aspects. The diff viewer only supports selection by line (since it's hand written, text level selection will require more work), but supports the usual shortcuts: CTRL+C or Command+C for copy and CTRL+A or Command+A for selecting all.

Arrows for scrolling should also work.


License
-------

You have a perpetual license to do whatever you want with this code, but if you do something cool with it a note would be nice, oh! and if you make some money out of it, buy me a beer.
