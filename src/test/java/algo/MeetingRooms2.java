package algo;

import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * @author Chao Chen
 */
class Interval {
    int start;
    int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

public class MeetingRooms2 {
    public int minMeetingRooms(Interval[] intervals) {
        PriorityQueue<Interval> pq = new PriorityQueue<>((a, b) -> a.end - b.end);
        Arrays.sort(intervals, (a, b) -> a.end - b.end);
        pq.offer(intervals[0]);
        for (int i = 1; i < intervals.length; i++) {
            Interval earliest = pq.poll();
            if (earliest.end <= intervals[i].start) {
                earliest.end = intervals[i].end;
            } else {
                pq.offer(intervals[i]);
            }
            pq.offer(earliest);
        }
        return pq.size();
    }
}
