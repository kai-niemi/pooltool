$('document').ready(function () {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))

    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })

    CodeMirror.fromTextArea(document.getElementById('modelEditor'), {
        lineNumbers: true,
        lineWrapping: true,
        readOnly: true,
        mode: 'text/x-yaml',
        theme: 'abbott'
    });
});

const EditorDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

EditorDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
                stompClient = Stomp.over(socket),
                _this = this;

        stompClient.log = (log) => {};

        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.refresh, function () {
                location.reload();
            });

            stompClient.subscribe(_this.settings.topics.toast, function (payload) {
                var _event = JSON.parse(payload.body);
                _this.handleToastUpdate(_event);
            });
        });
    },

    getElement: function (id) {
        return $('#' + id);
    },

    handleToastUpdate: function (event) {
        var _this = this;

        const toastElt = _this.getElement('toast');
        const toastBody = toastElt.find(".toast-body");
        toastBody.text(event.message);

        const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastElt);
        toastBootstrap.show();
    }
};

document.addEventListener('DOMContentLoaded', function () {
    new EditorDashboard({
        endpoints: {
            socket: '/pool-configurer',
        },

        topics: {
            refresh: '/topic/editor/refresh',
            toast: '/topic/toast',
        },
    });
});

