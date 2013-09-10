function UsersListCtrl($scope, $http, $timeout, $dialog, FlashService) {

    $scope.Math = window.Math;

    $scope.query = "";

    $scope.getUsers = function() {
        $http.get('/charging-server-rest-management/api/charging/users').
            success(function(data) {
                $scope.usersList = data;
                $scope.lastUpdated = new Date().toLocaleString();

                $scope.noOfPages = Math.ceil($scope.usersList.length / $scope.entryLimit);
            }).
            error(function(data) {
                // handle it
            });
    };

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    };

    $scope.setPage = function(pageNo) {
        $scope.currentPage = pageNo;
    };

    $scope.filter = function() {
        $timeout(function() { //wait for 'filtered' to be changed
            /* change pagination with $scope.filtered */
            $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
        }, 10);
    };

    // edit user balance modal -----------------------------------------------------------------------------------------

    $scope.editUserBalance = function (user) {
        $scope.selectedUser = user;
        $scope.selectedUser.newBalance = $scope.selectedUser.BALANCE;
        $scope.editingUserBalance = true;
    };

    $scope.closeEditUserBalance = function () {
        $scope.editingUserBalance = false;
    };

    $scope.opts = {
        backdropFade: true,
        dialogFade: true
    };

    $scope.setUserBalance = function(user) {
        $http.post('/charging-server-rest-management/api/charging/users/msisdn/' + user.MSISDN + '/balance/' + user.newBalance).
            success(function(data) {
                $scope.closeEditUserBalance();
                $scope.getUsers();
                FlashService.show("User " + user.MSISDN + " balance successfully updated to " + user.newBalance + ".", "success");
            }).
            error(function(data) {
                FlashService.show("User " + user.MSISDN + " balance failed to update.", "error");
            });
    };

    // add user --------------------------------------------------------------------------------------------------------

    $scope.showCreateNewUser = function () {
        $scope.newUser = [];
        $scope.creatingNewUser = true;
    };

    $scope.closeCreateNewUser = function () {
        $scope.creatingNewUser = false;
    };

    $scope.createNewUser = function(user) {
        if (!user.MSISDN) {
            FlashService.show("MSISDN value is required for creating new user.", "error");
            return false;
        }
        if (!user.BALANCE) {
            user.BALANCE = 0;
        }
        $http.put('/charging-server-rest-management/api/charging/users/msisdn/' + user.MSISDN + '/balance/' + user.BALANCE).
            success(function(data) {
                $scope.closeCreateNewUser();
                $scope.getUsers();
                FlashService.show("User " + user.MSISDN + " created successfully.", "success");
            }).
            error(function(data) {
                FlashService.show("Failure creating new user with MSISDN " + user.MSISDN + ".", "error");
            });
    };


    // delete user -----------------------------------------------------------------------------------------------------

    $scope.confirmUserDelete = function(user) {
        var title = 'Delete User ' + user.MSISDN;
        var msg = 'Are you sure you want to delete the user ' + user.MSISDN + '? This action cannot be undone.';
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    $http.delete('/charging-server-rest-management/api/charging/users/msisdn/' + user.MSISDN).
                        success(function(data) {
                            $scope.getUsers();
                            FlashService.show("User " + user.MSISDN + " deleted successfully.", "success");
                        }).
                        error(function(data) {
                            FlashService.show("Failed to delete User " + user.MSISDN + ".", "error");
                        });
                }
            });
    };

    $scope.confirmBalanceSanitize = function(user) {
        var title = 'Sanitize User ' + user.MSISDN + ' Balance';
        var msg = 'Sanitizing balance for user ' + user.MSISDN + ' will transfer ' + user.RESERVED + ' units which are reserved to his current balance of ' + user.BALANCE + '. New balance value will be ' + (user.BALANCE + user.RESERVED) + ".";
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Sanitize', cssClass: 'btn-primary'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    $http.post('/charging-server-rest-management/api/charging/users/msisdn/' + user.MSISDN + '/sanitize').
                        success(function(data) {
                            $scope.getUsers();
                            FlashService.show("User " + user.MSISDN + " balance sanitized successfully.", "success");
                        }).
                        error(function(data) {
                            FlashService.show("User " + user.MSISDN + " balance failed to sanitize.", "error");
                        });
                }
            });
    };

}

function UsersDetailCtrl($scope, $routeParams) {
    $scope.userId = $routeParams.userId;
}
