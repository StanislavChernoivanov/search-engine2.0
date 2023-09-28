package searchengine.dto.startIndexing;
import lombok.Data;
import lombok.Getter;

import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
@Data
public class SiteNode implements Comparable<SiteNode> {
    @Getter
    private final Set<SiteNode> childNodes = new TreeSet<>();
    @Getter
    private final URL url;
    public SiteNode(URL url) {
        this.url = url;
    }

    public void addChild(SiteNode child) {
        synchronized (childNodes) {
            childNodes.add(child);
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(url.toString());
        if (childNodes.size() > 0) {
            childNodes.forEach(c -> buffer.append(System.lineSeparator()).append(c));
        }
        return buffer.toString();
    }

    @Override
    public int compareTo(SiteNode o) {
        return url.toString().compareTo(o.url.toString());
    }
}
