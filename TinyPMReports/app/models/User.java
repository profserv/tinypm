package models;

import javax.persistence.*;

import play.db.jpa.*;


@Entity
@Table(name="tpm_users")
public class User extends Model{
    
    public String name;
    
    public User(String name)
    {
        this.name = name;
    }
    
}
