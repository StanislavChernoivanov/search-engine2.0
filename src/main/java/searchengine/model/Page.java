package searchengine.model;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;


@Entity
@Table(name = "Pages",
        indexes = @Index(name = "p_index",
                columnList = "path",
                unique = true))
@Getter
@Setter
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL)
    private Site site;

    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "Indexes",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
    private List<Lemma> lemmaList;

}
