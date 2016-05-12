'use strict';

(function () {
    var ws = new WebSocket('ws://localhost:8080/socket');
    ws.onmessage = function(event) {
        var input = JSON.parse(event.data);
        console.log(input);
        ws.send(input);
    };

    var button = document.getElementById("btn");
    button.onclick = function() {
        ws.send(JSON.stringify({"msg": "helooooo"}));
    };
})();