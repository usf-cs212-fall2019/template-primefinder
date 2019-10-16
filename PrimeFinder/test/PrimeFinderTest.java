import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Attempts to test if {@link PrimeFinder#findPrimes(int, int, int)} and
 * {@link WorkQueue#finish()} implementations are correct. Tests are not
 * perfect, and may not catch all implementation issues.
 *
 * @see PrimeFinder#findPrimes(int, int, int)
 * @see WorkQueue#finish()
 */
@TestMethodOrder(OrderAnnotation.class)
public class PrimeFinderTest {

	/** Maximum amount of time to wait per test. */
	public static final Duration GLOBAL_TIMEOUT = Duration.ofSeconds(60);

	/** Number of warmup rounds to run when benchmarking. */
	public static final int WARMUP_ROUNDS = 10;

	/** Number of timed rounds to run when benchmarking. */
	public static final int TIMED_ROUNDS = 20;

	/**
	 * Hard-coded set of known primes to compare against.
	 */
	public static final Set<Integer> KNOWN_PRIMES = Set.of(2, 3, 5, 7, 11, 13, 17,
			19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97,
			101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173,
			179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257,
			263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349,
			353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439,
			443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541,
			547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631,
			641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733,
			739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829,
			839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941,
			947, 953, 967, 971, 977, 983, 991, 997);

	/**
	 * Verify the single-threaded implementation also passes the tests
	 *
	 * @see PrimeFinder#trialDivision(int, int)
	 */
	@Test
	@Order(1)
	public void testTrialDivision() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.trialDivision(1, 1000);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	/**
	 * Verify the multithreaded implementation finds the correct primes with
	 * one worker thread.
	 *
	 * @see PrimeFinder#findPrimes(int, int, int)
	 */
	@RepeatedTest(3)
	@Order(2)
	public void testFindPrimes1Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 1);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	/**
	 * Verify the multithreaded implementation finds the correct primes with
	 * two worker threads.
	 *
	 * @see PrimeFinder#findPrimes(int, int, int)
	 */
	@RepeatedTest(3)
	@Order(3)
	public void testFindPrimes2Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 2);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	/**
	 * Verify the multithreaded implementation finds the correct primes with
	 * five worker threads.
	 *
	 * @see PrimeFinder#findPrimes(int, int, int)
	 */
	@RepeatedTest(3)
	@Order(4)
	public void testFindPrimes5Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 5);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	/**
	 * Test single and multithreaded results return the same results.
	 *
	 * @see PrimeFinder#findPrimes(int, int, int)
	 */
	@Test
	@Order(5)
	public void testSingleVersusMulti() {
		int max = 3000;
		int threads = 5;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> expected = PrimeFinder.trialDivision(1, max);
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, max, threads);

			Assertions.assertEquals(expected, actual);
		});
	}

	/**
	 * Verifies multithreading is faster than single threading for a large
	 * maximum value.
	 */
	@Test
	@Order(6)
	public void benchmarkSingleVersusMulti() {
		int max = 5000;
		int threads = 5;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			double single = new SingleBenchmarker().benchmark(max);
			double multi = new MultiBenchmarker(threads).benchmark(max);

			String debug = String.format("Single: %.4f Multi: %.4f, Speedup: %.4fx",
					single, multi, single / multi);

			Assertions.assertTrue(single >= multi, debug);
			System.out.println(debug);
		});
	}

	/**
	 * Verifies having one worker thread is faster than three worker threads.
	 */
	@Test
	@Order(7)
	public void benchmarkOneVersusThree() {
		int max = 5000;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			double multi1 = new MultiBenchmarker(1).benchmark(max);
			double multi3 = new MultiBenchmarker(3).benchmark(max);

			String debug = String.format("1 Thread: %.4f 3 Threads: %.4f, Speedup: %.4fx",
					multi1, multi3, multi1 / multi3);

			Assertions.assertTrue(multi1 > multi3, debug);
			System.out.println(debug);
		});
	}

	/**
	 * Verifies the work queue functions as expected.
	 *
	 * @throws InterruptedException
	 */
	@Test
	@Order(8)
	public void testWorkQueue() throws InterruptedException {
		int tasks = 10;
		int sleep = 10;
		int workers = tasks / 2;
		long timeout = Math.round(workers * sleep * 1.25);

		Assertions.assertTimeoutPreemptively(Duration.ofMillis(timeout), () -> {
			WorkQueue queue = new WorkQueue(workers);
			CountDownLatch count = new CountDownLatch(tasks);

			for (int i = 0; i < tasks; i++) {
				queue.execute(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(sleep);
							count.countDown();
						}
						catch (InterruptedException ex) {
							Assertions.fail("Task interrupted; queue did not complete in time.");
						}
					}
				});
			}

			queue.finish();
			queue.shutdown();

			// if you get stuck here then finish() isn't working
			count.await();
		});
	}

	/**
	 * Tests that the java.lang.Thread class does not appear in the implementation code.
	 *
	 * @throws IOException
	 */
	@Test
	@Order(9)
	public void testThreadClass() throws IOException {
		String source = Files.readString(Path.of(".", "src", "PrimeFinder.java"), StandardCharsets.UTF_8);
		Assertions.assertFalse(source.contains("import java.lang.Thread;"));
	}

	/**
	 * Used to benchmark code. Benchmarking results may be inconsistent, and are
	 * written to favor multithreading.
	 */
	private static abstract class Benchmarker {

		/**
		 * Method that returns a set of primes.
		 *
		 * @param max the maximum size to use
		 * @return set of primes
		 */
		public abstract Set<Integer> run(int max);

		/**
		 * Benchmarks the run method up to the max provided.
		 *
		 * @param max the maximum size to use
		 * @return average runtime
		 */
		public double benchmark(int max) {
			Integer first = Integer.MIN_VALUE;

			// warmup
			for (int i = 0; i < WARMUP_ROUNDS; i++) {
				Set<Integer> results = run(max);
				first = Math.max(first, results.iterator().next());
			}

			// timed
			Instant start = Instant.now();

			for (int i = 0; i < TIMED_ROUNDS; i++) {
				Set<Integer> results = run(max);
				first = Math.max(first, results.iterator().next());
			}

			Instant end = Instant.now();

			if (!KNOWN_PRIMES.contains(first)) {
				Assertions.fail("Unexpected test result. Make sure results are correct before running this test.");
			}

			// averaged result
			Duration elapsed = Duration.between(start, end);
			return (double) elapsed.toMillis() / TIMED_ROUNDS;
		}
	}

	/**
	 * Used to benchmark single threaded code.
	 */
	private static class SingleBenchmarker extends Benchmarker {

		@Override
		public Set<Integer> run(int max) {
			return PrimeFinder.trialDivision(1, max);
		}

	}

	/**
	 * Used to benchmark multithreaded code.
	 */
	private static class MultiBenchmarker extends Benchmarker {

		/** Number of threads to use. */
		private final int threads;

		/**
		 * Initializes the number of threads.
		 *
		 * @param threads the number of threads to use
		 */
		public MultiBenchmarker(int threads) {
			this.threads = threads;
		}

		@Override
		public Set<Integer> run(int max) {
			return PrimeFinder.findPrimes(1, max, threads);
		}
	}
}
