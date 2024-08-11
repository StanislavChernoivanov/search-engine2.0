package searchengine.model.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemmas", uniqueConstraints =
@UniqueConstraint(name = "l_index", columnNames = {"lemma", "site_id"}),
        indexes = @javax.persistence.Index(name = "lemma_index", columnList = "lemma"))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Lemma implements Comparable<Lemma>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_index_site2"), name = "site_id")
    @EqualsAndHashCode.Include
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    @EqualsAndHashCode.Include
    private String lemma;
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE)
    private List<Indexes> indexesList = new ArrayList<>();


    @Override
    public int compareTo(Lemma o) {
        if (getFrequency() == o.getFrequency()) return Integer.compare(id, o.id);
        return Integer.compare(this.frequency, o.frequency);
    }
}
