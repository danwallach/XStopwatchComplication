# X-Stopwatch and Timer Complications

This is a pair of complications for Android Wear 2.0, installed as a
single app. Multiple instances of each may be active
simultaneously. Once you add the complication to a watchface, clicking
it will launch an activity where you can interact with it. When you go
back to the watchface, everything should be properly updated.

For earlier versions of Android Wear, I developed
[a similar app](http://www.cs.rice.edu/~dwallach/xstopwatch/), which
communicated with my
[CalWatch](http://www.cs.rice.edu/~dwallach/calwatch/) watchface. The
code here has a lot in common with that project, but it's really a
new implementation to support the new complication model of
Wear 2.0. I haven't yet ported CalWatch to Android Wear 2.0.

To any software developer / reader: everything here is implemented in
Kotlin, using their fancy Anko extensions that make it much easier to
implement Android apps. I didn't use the Anko layout system,
though. Instead, I'm using the bog-standard XML Android layout engine,
albeit with the brand-new constraint system, as supported by Android
Studio. The code should be perfectly readable by any
Android-experienced Java programmer.
