package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;


@Entity
@Table(name="tpm_user_stories")
public class UserStory extends Model{
    
    public String name;
    public String description;
    
    @ManyToOne
    @JoinColumn(name="projectId")
    public Project project;
    
    @OneToMany(mappedBy="story")
    public List<Task> tasks;
            
    public UserStory(String name, String description)
    {
        this.name = name;
        this.description = description;
    }
    
}
