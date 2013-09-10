var app = angular.module('chargingserver', ['ngResource', 'ui.bootstrap']).
    config(['$routeProvider', function($routeProvider) {
        $routeProvider.
            when('/users', {templateUrl: 'modules/users-list.html', controller: UsersListCtrl}).
            when('/users/:userId', {templateUrl: 'modules/user-detail.html', controller: UsersDetailCtrl}).
            when('/services', {templateUrl: 'modules/services-list.html', controller: UsersListCtrl}).
            when('/promotions', {templateUrl: 'modules/promotions-list.html', controller: UsersListCtrl}).
            otherwise({redirectTo: '/users'});
    }]);

app.filter('startFrom', function() {
    return function(input, start) {
        if(input) {
            start = +start; //parse to int
            return input.slice(start);
        }
        return [];
    }
});

app.factory("FlashService", function($rootScope, $timeout) {
    return {
        show: function(message, type) {
            $rootScope.flash = {type: type, message: message};
            $timeout(function() {
                $rootScope.flash = {type: "", message: ""};
            }, 3000);
        },
        clear: function() {
            $rootScope.flash = {type: "", message: ""};
        }
    }
});