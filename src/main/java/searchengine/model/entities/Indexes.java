package searchengine.model.entities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;

@Entity
@Table(name = "indexes")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Indexes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_page"), name = "page_id")
    @EqualsAndHashCode.Include
    private Page page;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_lemma"), name = "lemma_id")
    @EqualsAndHashCode.Include
    private Lemma lemma;
    @Column(name = "ranks")
    private float rank;

}