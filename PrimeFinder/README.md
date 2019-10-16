# Prime Finder

For this homework assignment, you must:

  1. Add a `pending` variable to the `WorkQueue` class to track unfinished work, and implement a `finish()` method that waits until there is no more pending work. 

  2. Implement the multithreaded `findPrimes(int, int, int)` method in `PrimeFinder` using a `WorkQueue`. There should be 1 task or `Runnable` object for each number being tested, and the `run()` method of these objects should use the `isPrime(int)` method.
  
## Requirements

The official name of this homework is `PrimeFinder`. This should be the name you use for your Eclipse Java project and the name you use when running the homework test script.

See the [Homework Guides](https://usf-cs212-fall2019.github.io/guides/homework/) for additional details on homework requirements and submission.
