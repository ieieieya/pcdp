package edu.coursera.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * minproject2 main file.
 * <p>
 * Class wrapping methods for implementing reciprocal array sum in parallel.
 * <p>
 * In my cases. the answers are pretty environment sensitive.
 * And hyper-threading can introduce performance issues.
 *
 * NOTE: The teaching staff claims projects' performance are not meant to be run on your local machine
 * since different hardware environments vary greatly
 *
 * <p>
 * environment: 4 cores, 8 Threads (Hyper-Threading)
 * <p>
 * Test Case1 is easy.
 * Test Case2 most of time fails to pass which achieves 3.7 score, and occasionally jump to 4.8, i.e. the pass-line.
 */
public final class ReciprocalArraySum {

    /**
     * Default constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Sequentially compute the sum of the reciprocal values for a given array.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Computes the size of each chunk, given the number of chunks to create
     * across a given number of elements.
     *
     * @param nChunks   The number of chunks to create
     * @param nElements The number of elements to chunk across
     * @return The default chunk size
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Integer ceil
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Computes the inclusive element index that the provided chunk starts at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the start of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The inclusive index that this chunk starts at in the set of
     * nElements
     */
    private static int getChunkStartInclusive(final int chunk,
                                              final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Computes the exclusive element index that the provided chunk ends at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the end of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The exclusive end index for this chunk
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
                                            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * This class stub can be filled in to implement the body of each task
     * created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        // this parameter is critical in most cases.
        static final int SEQUENTIAL_THRESHOLD = 2000;

        /**
         * Constructor.
         *
         * @param setStartIndexInclusive Set the starting index to begin
         *                               parallel traversal at.
         * @param setEndIndexExclusive   Set ending index for parallel traversal.
         * @param setInput               Input values
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                               final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Getter for the value produced by this task.
         *
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            if (endIndexExclusive - startIndexInclusive < SEQUENTIAL_THRESHOLD) {
                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    if (input[i] != 0) {
                        value += 1 / input[i];
                    } else {
                        System.exit(1);
                    }
                }
            } else {
                int mid = (startIndexInclusive + endIndexExclusive) / 2;
                ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive, mid, input);
                ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, endIndexExclusive, input);
                left.fork();
                right.compute();
                left.join();
                value = left.value + right.value;
            }
        }
    }

    /**
     * TODO: Modify this method to compute the same reciprocal sum as
     * seqArraySum, but use two tasks running in parallel under the Java Fork
     * Join framework. You may assume that the length of the input array is
     * evenly divisible by 2.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        ForkJoinPool pool = new ForkJoinPool(2);
        ReciprocalArraySumTask rast = new ReciprocalArraySumTask(0, input.length, input);
        pool.invoke(rast);
        pool.shutdown();
        return rast.getValue();
    }

    /**
     * TODO: Extend the work you did to implement parArraySum to use a set
     * number of tasks to compute the reciprocal array sum. You may find the
     * above utilities getChunkStartInclusive and getChunkEndExclusive helpful
     * in computing the range of element indices that belong to each chunk.
     *
     * @param input    Input array
     * @param numTasks The number of tasks to create
     * @return The sum of the reciprocals of the array input
     */
    protected static double parManyTaskArraySum(final double[] input,
                                                final int numTasks) {

        double sum = 0.;
        // ForkJoinPool pool = new ForkJoinPool(nt); // you can use ForkJoinPool too.
        int nt = numTasks / 2;
        List<ReciprocalArraySumTask> rastList = new ArrayList<>(nt);
        for (int i = 0; i < nt; i++) {
            int low = getChunkStartInclusive(i, nt, input.length);
            int high = getChunkEndExclusive(i, nt, input.length);
            ReciprocalArraySumTask rast = new ReciprocalArraySumTask(low, high, input);
            rastList.add(rast);
        }
        for (int i = 0; i < nt - 1; i++) {
            rastList.get(i).fork();
        }
        rastList.get(rastList.size() - 1).compute();
        for (int i = 0; i < nt - 1; i++) {
            rastList.get(i).join();
        }
        for (int i = 0; i < rastList.size(); i++) {
            sum += rastList.get(i).getValue();
        }
//        pool.shutdown();
        return sum;

    }
}
