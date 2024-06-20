package searchengine.utils.startIndexing;

import java.net.URL;
import java.util.*;

public class UrlContainer {
    private static final Map<String, Set<String>> URL_CONTAINER;
    static {
        URL_CONTAINER = new HashMap<>();

    }

    static void addToContainer(URL url) {
        if (hostIsContained(url)) URL_CONTAINER.get(url.getHost()).add(url.toString());
        else {
            Set<String> urls = new HashSet<>();
            urls.add(url.toString());
            URL_CONTAINER.put(url.getHost(), urls);
        }
    }

    static boolean urlIsContained(URL url) {
        if(!hostIsContained(url)) return false;
        return URL_CONTAINER.get(url.getHost()).stream()
            .anyMatch(u -> u.equals(url.toString()));
    }

    private static boolean hostIsContained(URL url) {
        return URL_CONTAINER.keySet().stream()
                .anyMatch(h -> h.equals(url.getHost()));

    }
    static void clear(URL url){
        URL_CONTAINER.remove(url.getHost());
    }

//        private static long getMemoryOfURLContainer() {
//            long totalSum = 0;
//            for(Set<String> set : URL_CONTAINER.values()) {
//                Optional<Integer> sumOpt = set.stream().map(String::length).reduce(Integer::sum);
//                totalSum= totalSum + sumOpt.get();
//            }
//            totalSum = totalSum / 1048578;
//            return totalSum;
//        }

}
