package org.example.user;

public class User {
    public String username;
    public UserStates state = UserStates.DISCONNECTED;

    public void updateUsername(String username){
        this.username = username;
    }

    public void updateState(UserStates state){
        this.state = state;
    }
}
