package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "indexes")
@Getter
@Setter
public class Indexes implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_page"), name = "page_id")
    private Page page;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JoinColumn(foreignKey=@ForeignKey(name = "FK_index_lemma"), name = "lemma_id")
    private Lemma lemma;
    @Column(name = "ranks")
    private float rank;

}