class ChatClient {
    constructor(userName) {
        // Use the bind.js micro-library to update the DOM
        // simply by updating a data object.
        // https://github.com/remy/bind.js
        this.model = Bind(
            // First argument is the data model
            {
                users: [],
                username: userName,
                userId: null,
                currentRoom: {title: "", posts: []},
                rooms: [],
                newRoom: null,
                newPost: null,
            },
            // Second argument maps data items in that model
            // to stuff in the DOM
            {
                users: {
                    dom: '#users',
                    transform: (value) => `<li><a href="${value}">${value}<a/></li>`
                },
                username: {
                    dom: '#username',
                    transform: (value) => value ? value : 'Disconnected'
                },
                "currentRoom.title": "#currentRoom",
                "currentRoom.posts": {
                    dom: "#currentPosts",
                    transform: (value) => `
                    <li class="list-group-item">${value.from}: ${value.body}</li>
`
                },
                newRoom: '#newRoom',
                newPost: '#newPost',
                rooms: {
                    dom: '#rooms',
                    transform: (value) => `
                        <li><a href="${value.id}">${value.title}</a></li>
`
                }
            });

        // Make a websocket connection and re-map all of the
        // websocket callbacks to ChatClient methods.
        this.socket = new WebSocket("ws://localhost:3000/chat");
        this.socket.onopen = () => this.onOpen();
        this.socket.onclose = () => this.onClose();
        this.socket.onmessage = (event) => this.onMessage(event);
        this.socket.onerror = (event) => this.onError(event);
    }

    onOpen() {
        this.socket.send(JSON.stringify(
            {type: "CONNECT", username: this.model.username}
        ));
    }

    onClose() {
        console.log("You were disconnected from the server");
    }

    onError(event) {
        console.log("Error:", event);
    }

    onUnload() {
        this.socket.onclose = () => null;
        this.socket.close()
    }

    onMessage(event) {
        let message = JSON.parse(event.data);
        switch (message.type) {
            case "CLIENT_LIST":
                return this.listUsers(message);
            case "ROOM_LIST":
                return this.listRooms(message);
            case "ROOM_POSTS":
                return this.listPosts(message);
        }
    }

    listUsers(message) {
        this.model.users = message.body.filter ( username => username != this.model.username);
    }

    listRooms(message) {
        this.model.rooms = message.body;
    }

    listPosts(message) {
        this.model.currentRoom.id = message.body.id;
        this.model.currentRoom.title = message.body.title;
        this.model.currentRoom.posts = message.body.posts;
    }

    addRoom() {
        // If the form field is empty, don't add a room
        if (this.model.newRoom) {
            let obj = {type: "ADD_ROOM", title: this.model.newRoom};
            this.socket.send(JSON.stringify(obj));
            this.model.newRoom = "";
        }
    }

    setRoom(roomId) {
        console.log("setting", roomId);
        this.socket.send(JSON.stringify({type: "GET_ROOM", roomId: roomId}))
    }

    addPost() {
        // If the form field is empty, don't add a post
        let to = ""
        if (this.model.currentRoom.id.charAt(0) == "@") {
            to = this.model.currentRoom.id.substring(this.model.currentRoom.id.indexOf("#"))
        }
        if (this.model.newPost) {
            let obj = {
                type: "ADD_POST", body: this.model.newPost,
                roomId: this.model.currentRoom.id, from: this.model.username, to: to
            };
            this.socket.send(JSON.stringify(obj));
            this.model.newPost = "";
        }
    }

    setPeerChat(targetUser) {
        console.log("Starting peer chat")
        this.socket.send(JSON.stringify({ type: "GET_PEER", from: this.model.username, to: targetUser}))
    }
}

document.addEventListener("DOMContentLoaded", () => {

    let username = location.hash.substring(1);
    // If no # in URL, don't connect
    if (!username) return;
    let chat = new ChatClient(username);
    window.onbeforeunload = () => chat.onUnload(); // Cleanup on exit

    // Handle the add room input box form submission
    document.querySelector('#addRoom').addEventListener("submit", (event) => {
        event.preventDefault();
        chat.addRoom();
    });

    // Handle clicking on a room in the list
    document.querySelector("#rooms").addEventListener("click", (event) => {
        event.preventDefault();
        chat.setRoom(event.target.getAttribute("href"));
    });

    // Handle adding a post to the current room
    document.querySelector('#addPost').addEventListener("submit", (event) => {
        event.preventDefault();
        chat.addPost();
    });

    // Handle clicking on a user
    document.querySelector('#users').addEventListener("click", (event) => {
        event.preventDefault();
        chat.setPeerChat(event.target.getAttribute("href"))
    })
});
