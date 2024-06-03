package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "pages", indexes = @javax.persistence.Index(name = "path_index", columnList = "path"),
        uniqueConstraints = @UniqueConstraint(name = "page_site", columnNames = {"path", "site_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Page implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"), name = "site_id")
    @EqualsAndHashCode.Include
    private Site site;
    @EqualsAndHashCode.Include
    @Column(length = 500)
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE)
    private List<Indexes> indexList = new ArrayList<>();

}
