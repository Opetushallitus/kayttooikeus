import Bacon from 'baconjs'

const locationBus = new Bacon.Bus();

function parseQuery(qstr) {
    var query = {};
    var a = qstr.substr(1).split('&');
    for (var i = 0; i < a.length; i++) {
        var b = a[i].split('=');
        query[decodeURIComponent(b[0])] = decodeURIComponent(b[1] || '')
    }
    return query
}

function parseLocation(location) {
    return {
        path: location.pathname,
        params: parseQuery(location.search),
        queryString: location.search || ''
    }
}

const parsePath = (path) => {
    let a = document.createElement('a');
    a.href = path;
    return parseLocation(a);
};

export const navigateTo = function (path) {
    history.pushState(null, null, path);
    locationBus.push(parsePath(path));
};

window.onpopstate = function() {
    locationBus.push(parseLocation(document.location));
};


export const locationP = locationBus.toProperty(parseLocation(document.location));
export const showError = (error) => locationBus.error(error);
