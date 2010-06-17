The org.clapper.classutil (ClassUtil) library is a Scala package that
provides various class location and class generation capabilities, including:

* Methods to locate and filter classes quickly, at runtime--more quickly, in
  fact, than can be done with the JVM's runtime reflection capabilities.
* Methods for converting Scala maps into Java Beans, on the fly--which can be
  useful when generating data for use with APIs (e.g., template APIs) that
  accept Java Beans, but not maps.
