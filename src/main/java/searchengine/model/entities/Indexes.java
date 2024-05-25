package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "indexes")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Indexes implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_page"), name = "page_id")
    @EqualsAndHashCode.Include
    private Page page;
    @ManyToOne()
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_lemma"), name = "lemma_id")
    @EqualsAndHashCode.Include
    private Lemma lemma;
    @Column(name = "ranks")
    private float rank;

}