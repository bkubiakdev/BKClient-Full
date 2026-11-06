package dev.bkclient.utility.network;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
/** Bounded FIFO delay queue: packets mature individually, without burst windows. */
public final class FakeLagBatch<T> {
    private record Entry<T>(T item,long started,long deadline) {}
    private final int capacity;
    private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
    public FakeLagBatch(int capacity) { if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity; }
    public static double holdMillisFor(double delay) { return Double.isFinite(delay)?Math.clamp(delay,50,400):200; }
    public boolean isEmpty() { return queue.isEmpty(); }
    public boolean isFull() { return queue.size()>=capacity; }
    public void add(T item,long now,double delay) {
        if(isFull())throw new IllegalStateException("Queue capacity exceeded");
        long deadline=now+(long)(holdMillisFor(delay)*1_000_000);
        if(!queue.isEmpty())deadline=Math.max(deadline,queue.getLast().deadline());
        queue.addLast(new Entry<>(item,now,deadline));
    }
    public double holdMillis() { var head=queue.peekFirst();return head==null?0:(head.deadline()-head.started())/1_000_000.0; }
    public boolean due(long now) { return !queue.isEmpty() && now>=queue.getFirst().deadline(); }
    public float remainingFraction(long now) { var head=queue.peekFirst();return head==null?0:Math.clamp((float)(head.deadline()-now)/(head.deadline()-head.started()),0,1); }
    public List<T> drainDue(long now) { var result=new ArrayList<T>();while(due(now))result.add(queue.removeFirst().item());return result; }
    /** Classic pulse: expire the first packet, then replay the whole snapshot in FIFO order. */
    public List<T> drainBatchDue(long now) { return due(now) ? drain() : java.util.List.of(); }
    public List<T> drain() { var result=new ArrayList<T>();while(!queue.isEmpty())result.add(queue.removeFirst().item());return result; }
    public void clear() { queue.clear(); }
}
