package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


@Entity
@Table(name = "pages",
        indexes = @Index(name = "p_index",
                columnList = "path",
                unique = true))
@Getter
@Setter
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_site_index"))
    private Site site;

    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

//    @OneToMany(mappedBy = "pages", cascade = CascadeType.ALL)
//    private List<Indexes> indexList = new ArrayList<>();

}
