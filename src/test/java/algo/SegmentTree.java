package algo;

public class SegmentTree {
    int[] tree, arr;

    public SegmentTree(int[] array) {
        int n = array.length;
        tree = new int[4 * n];
        arr = array;
        buildTree(0, 0, n - 1);
        printTree();
    }

    public void printTree() {
        int level = 0;
        int nodesInCurrentLevel = 1;
        int nodesInNextLevel = 0;
        System.out.println("Segment Tree (Level by Level):");
        for (int i = 0; i < tree.length; i++) {
            if (nodesInCurrentLevel == 0) {
                System.out.println();
                level++;
                nodesInCurrentLevel = nodesInNextLevel;
                nodesInNextLevel = 0;
            }
            System.out.print(tree[i] + " ");
            nodesInCurrentLevel--;
            nodesInNextLevel += 2; // Each node has two children
        }
        System.out.println();
    }

    public void buildTree(int node, int start, int end) {
        int left = 2 * node + 1;
        int right = 2 * node + 2;
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            buildTree(left, start, mid);
            buildTree(right, mid + 1, end);
            tree[node] = tree[left] + tree[right];
        }

    }

    public int rangeQuery(int node, int start, int end, int l, int r) {
        if (r < start || end < l)
            return 0;
        if (l <= start && end <= r)
            return tree[node];

        int mid = (start + end) / 2;
        int left = 2 * node + 1;
        int right = 2 * node + 2;
        return rangeQuery(left, start, mid, l, r) + rangeQuery(right, mid + 1, end, l, r);
    }


    public void update(int node, int start, int end, int index, int newValue) {
        if (start == end) {
            arr[index] = newValue;
            tree[node] = newValue;
        } else {
            int mid = (start + end) / 2;
            int left = 2 * node + 1;
            int right = 2 * node + 2;
            if (mid > index) update(left, start, mid, index, newValue);
            else update(right, mid + 1, end, index, newValue);
            tree[node] = tree[left] + tree[right];
        }

    }

    public static void main(String[] args) {
        // Example usage
        int[] heights = {1, 3, 2, 4, 5, 6, 8, 1};
        int[][] queries = {{0, 1}, {1, 2}, {2, 3}};

        SegmentTree segmentTree = new SegmentTree(heights);
        for (int i = 0; i < queries.length; i++) {
            int result = segmentTree.rangeQuery(0, 0, heights.length - 1, queries[i][0], queries[i][1]);
            System.out.println("Result for query " + i + ": " + result);
        }
    }
}
