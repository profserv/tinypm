package models;

import javax.persistence.*;

import play.db.jpa.*;

@Entity
@Table(name="tpm_tasks")
public class Task extends Model{
    
    public String name;
    public String description;
    
    @ManyToOne
    @JoinColumn(name="userStoryId")
    public UserStory story;
    
    public Task(String name, String description)
    {
        this.name = name;
        this.description = description;
    }
    
}
