package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;



@Entity
@Table(name = "pages",
        indexes = @Index(name = "p_index",
                columnList = "path",
                unique = true))
@Getter
@Setter
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"))
    private Site site;

    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Page page = (Page) obj;
        return path.equals(page.path);
    }

    //    @OneToMany(mappedBy = "pages", cascade = CascadeType.ALL)
//    private List<Indexes> indexList = new ArrayList<>();

}
