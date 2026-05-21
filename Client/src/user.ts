interface IUser {
    username: string;
    state: UserStates;
}

export enum UserStates {
    DISCONNECTED,
    CONNECTED,
}

export class User {
    private static readonly DEFAULT_USERNAME: string = "Bob";

    private data: IUser = { username: User.DEFAULT_USERNAME, state: UserStates.DISCONNECTED } as IUser;

    public changeUsername(username: string) {
        if (this.data.state === UserStates.CONNECTED) {
            alert("You can't change username during open session!");
            return;
        }
        if (username === "" || username === undefined) {
            alert("Wrong username!");
            return;
        }

        this.data.username = username;
    }

    public getUsername() {
        return this.data.username;
    }

    public isUserConnected() {
        return this.data.state;
    }

    public getDefaultUsername() {
        return User.DEFAULT_USERNAME;
    }

    public updateUserState(state: UserStates){
        this.data.state = state;
    }
}
