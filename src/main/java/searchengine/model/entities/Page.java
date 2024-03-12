package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "pages",uniqueConstraints =
@UniqueConstraint(name = "page_site", columnNames = {"path", "site_id"}))
@Getter
@Setter
public class Page implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"))
    private Site site;
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Override
    public int hashCode() {
        char[] i = path.toCharArray();
        StringBuilder builder = new StringBuilder();
        for(int j : i) {
            builder.append(j);
        }
        return id + Integer.parseInt(builder.toString()
                .toCharArray().length > 7 ? builder.substring(0, 6) : builder.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Page page = (Page) obj;
        return this.path.equals(page.path);
    }

        @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<Indexes> indexList = new ArrayList<>();

}
