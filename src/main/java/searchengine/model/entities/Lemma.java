package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "lemmas",
        indexes = @Index(name = "l_index",
                columnList = "lemma",
                unique = true))
@Getter
@Setter
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_index_site2"))
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    private int frequency;
//    @OneToMany(mappedBy = "lemmas", cascade = CascadeType.ALL)
//    private List<Indexes> indexesList = new ArrayList<>();

}
