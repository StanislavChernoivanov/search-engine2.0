package searchengine.model;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "Lemmas")
@Getter
@Setter
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    private int frequency;
}
