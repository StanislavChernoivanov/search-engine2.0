package searchengine.search;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Page;

import java.util.*;

@RequiredArgsConstructor
@Log4j2
public class Relevance {

    private final Map<Page, Float> map;

    public Map<Page, Float> getRelativeRelevance() {
        Optional<Float> max = map.values().stream().
                max((Float::compareTo));
        if (max.isPresent()) {
            Map<Page, Float> relativeRelevanceMap =
                    new TreeMap<>((o1, o2) -> map.get(o1).compareTo(map.get(o2)));
            map.keySet().forEach(f -> relativeRelevanceMap.put(f, map.get(f) / max.get()));
            return relativeRelevanceMap;
        } else throw new RuntimeException();
    }
}
