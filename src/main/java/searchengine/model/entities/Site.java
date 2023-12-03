package searchengine.model.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "sites")
@Getter
@Setter
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private EnumStatus status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error", nullable = false)
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(70)")
    private String name;

}