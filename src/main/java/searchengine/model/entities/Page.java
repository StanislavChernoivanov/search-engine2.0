package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "pages", indexes = @javax.persistence.Index(name = "path_index", columnList = "path"),
        uniqueConstraints = @UniqueConstraint(name = "page_site", columnNames = {"path", "site_id"}))

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"), name = "site_id")
    @EqualsAndHashCode.Include
    private Site site;
    @EqualsAndHashCode.Include
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE)
    private List<Indexes> indexList = new ArrayList<>();

}
