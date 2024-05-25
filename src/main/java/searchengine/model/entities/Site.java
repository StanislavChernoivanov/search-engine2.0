package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "sites",
        indexes = @Index(name = "s_index",
                columnList = "url",
                unique = true))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Site implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private EnumStatus status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    @EqualsAndHashCode.Include
    private String url;
    @Column(columnDefinition = "VARCHAR(70)")
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE)
    private List<Page> pageList = new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE)
    private List<Lemma> lemmaList = new ArrayList<>();

}