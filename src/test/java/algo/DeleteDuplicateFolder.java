package algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class DeleteDuplicateFolder {
    class Node {
        String name;
        String signature;
        TreeMap<String, Node> children = new TreeMap<>();

        public Node(String name) {
            this.name = name;

        }
    }

    public List<List<String>> deleteDuplicateFolder(List<List<String>> paths) {

        Node root = new Node("");
        for (List<String> l : paths) {
            Node cur = root;
            for (String ch : l) {
                cur.children.putIfAbsent(ch, new Node(ch));
                cur = cur.children.get(ch);
            }
        }
        HashMap<String, Integer> countMap = new HashMap<>();
        dfs(root, countMap);
        List<List<String>> ans = new ArrayList<>();
        dfs2(root, countMap, ans, new ArrayList<>());
        return ans;
    }

    private void dfs(Node root, HashMap<String, Integer> countMap) {
        if (root.children.isEmpty()) {
            root.signature = "";
            return ;
        }
        StringBuilder sb = new StringBuilder();
        for (Node node : root.children.values()) {
            dfs(node, countMap);
            sb.append(node.name).append("(").append(node.signature).append(")");
        }
        root.signature = sb.toString();
        countMap.put(root.signature, countMap.getOrDefault(root.signature, 0) + 1);

    }

    private void dfs2(Node root, HashMap<String, Integer> countMap, List<List<String>> ans,List<String> cur) {
        if (!root.children.isEmpty()&&countMap.getOrDefault(root.signature,0)>1){
            return;
        }
        cur.add(root.name);
        ans.add(new ArrayList<>(cur));
        for(Node ch:root.children.values()){
            dfs2(ch,countMap,ans,cur);
        }
        cur.remove(cur.size()-1);
    }
}
