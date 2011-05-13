package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

/**
 *
 * @author jhohenstein
 */
@Entity
@Table(name="tpm_projects")
public class Project extends Model{
    
    public String name;
    public String description;
    
    @OneToMany(mappedBy="project")
    public List<UserStory> stories;
    
    public Project(String name, String description)
    {
        this.name = name;
        this.description = description;
    }
    
}
