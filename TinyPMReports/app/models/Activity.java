package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
@Table(name="tpm_activities")
public class Activity extends Model{
    
    public Date date;
    public Date monthDate;
    public double timeSpent;
    
    @ManyToOne
    @JoinColumn(name="userId")
    public User user;
    
    @ManyToOne
    @JoinColumn(name="taskId")
    public Task task;

    public Activity(Date date, Date monthDate, double timeSpent)
    {
        this.date = date;
        this.monthDate = monthDate;
        this.timeSpent = timeSpent;
    }
    
}
