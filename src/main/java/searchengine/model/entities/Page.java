package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "pages", indexes = @javax.persistence.Index(name = "path_index", columnList = "path"), uniqueConstraints =
        @UniqueConstraint(name = "page_site", columnNames = {"path", "site_id"}))

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"))
    @EqualsAndHashCode.Include
    private Site site;
    @EqualsAndHashCode.Include
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

//    @Override
//    public int hashCode() {
//        char[] i = path.toCharArray();
//        StringBuilder builder = new StringBuilder();
//        for(int j : i) {
//            builder.append(j);
//        }
//        return site.getId() + Integer.parseInt(builder.toString()
//                .toCharArray().length > 7 ? builder.substring(0, 6) : builder.toString());
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (obj == null || getClass() != obj.getClass()) return false;
//        Page page = (Page) obj;
//        return this.path.equals(page.path);
//    }

        @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<Indexes> indexList = new ArrayList<>();

}
