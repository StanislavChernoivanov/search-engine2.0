package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "indexes",uniqueConstraints =
        @UniqueConstraint(name = "indexes_ident", columnNames = {"lemma_id", "page_id"}))
@Getter
@Setter
public class Indexes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_page"), name = "page_id")
    private Page page;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_lemma"), name = "lemma_id")
    private Lemma lemma;
    @Column(name = "ranks")
    private float rank;

}