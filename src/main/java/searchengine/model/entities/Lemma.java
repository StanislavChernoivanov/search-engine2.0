package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemmas",uniqueConstraints =
@UniqueConstraint(name = "l_index", columnNames = {"lemma", "site_id"}))
@Getter
@Setter
public class Lemma implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_index_site2"))
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.DETACH)
    private List<Indexes> indexesList = new ArrayList<>();
    @Override
    public int hashCode() {
        char[] i = lemma.toCharArray();
        StringBuilder builder = new StringBuilder();
        for(int j : i) {
            builder.append(j);
        }
        return id + Integer.parseInt(builder.toString()
                .toCharArray().length > 7 ? builder.substring(0, 6) : builder.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Lemma lemma = (Lemma) obj;
        return this.lemma.equals(lemma.lemma);
    }
}
