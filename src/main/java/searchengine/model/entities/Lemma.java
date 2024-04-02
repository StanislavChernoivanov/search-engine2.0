package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemmas",uniqueConstraints =
@UniqueConstraint(name = "l_index", columnNames = {"lemma", "site_id"}),
        indexes = @javax.persistence.Index(name = "lemma_index", columnList = "lemma"))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_index_site2"))
    @EqualsAndHashCode.Include
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    @EqualsAndHashCode.Include
    private String lemma;
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<Indexes> indexesList = new ArrayList<>();

}
