package janusgraph.util.batchimport.unsafe.stage;

import janusgraph.util.batchimport.unsafe.stats.StepStats;

/**
 * One step in {@link Stage}, where a {@link Stage} is a sequence of steps. Each step works on batches.
 * Batches are typically received from an upstream step, or produced in the step itself. If there are more steps
 * {@link #setDownstream(Step) downstream} then processed batches are passed down. Each step has maximum
 * "work-ahead" size where it awaits the downstream step to catch up if the queue size goes beyond that number.
 *
 * Batches are associated with a ticket, which is simply a long value incremented for each batch.
 * It's the first step that is responsible for generating these tickets, which will stay unchanged with
 * each batch all the way through the stage. Steps that have multiple threads processing batches can process
 * received batches in any order, but must make sure to send batches to its downstream
 * (i.e. calling {@link #receive(long, Object)} on its downstream step) ordered by ticket.
 *
 * @param <T> the type of batch objects received from upstream.
 */
public interface Step<T> extends Parallelizable, AutoCloseable, Panicable
{
    /**
     * Whether or not tickets arrive in {@link #receive(long, Object)} ordered by ticket number.
     */
    int ORDER_SEND_DOWNSTREAM = 0x1;
    int RECYCLE_BATCHES = 0x2;

    /**
     * Starts the processing in this step, such that calls to {@link #receive(long, Object)} can be accepted.
     *
     * @param orderingGuarantees which ordering guarantees that will be upheld.
     */
    void start(int orderingGuarantees);

    /**
     * @return name of this step.
     */
    String name();

    /**
     * Receives a batch from upstream, queues it for processing.
     *
     * @param ticket ticket associates with the batch. Tickets are generated by producing steps and must follow
     * each batch all the way through a stage.
     * @param batch the batch object to queue for processing.
     * @return how long it time (millis) was spent waiting for a spot in the queue.
     */
    long receive(long ticket, T batch);

    /**
     * @return statistics about this step at this point in time.
     */
    StepStats stats();

    /**
     * Called by upstream to let this step know that it will not send any more batches.
     */
    void endOfUpstream();

    /**
     * @return {@code true} if this step has received AND processed all batches from upstream, or in
     * the case of a producer, that this step has produced all batches.
     */
    boolean isCompleted();

    /**
     * Called by the {@link Stage} when setting up the stage. This will form a pipeline of steps,
     * making up the stage.
     * @param downstreamStep {@link Step} to send batches to downstream.
     */
    void setDownstream(Step<?> downstreamStep);

    /**
     * Closes any resources kept open by this step. Called after a {@link Stage} is executed, whether successful or not.
     */
    @Override
    void close() throws Exception;
}
