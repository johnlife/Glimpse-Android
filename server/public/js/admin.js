var users = {}
var sessionUsername = sessionUsername || ""

function showUsers() {
	$("#loginform").hide()
	$("#dashboard").show()
	$("#edit-user-page").hide()

	$.get("/api/v1/users", function(data) {
		//console.log(data)
		$("#users-table").html("")
		users = {}
		$.each(data, function(i, user) {
			users[user.email] = user
			$("#users-table").append(
				"<tr><td>"+user.email+"</td><td>"+
				(user.whitelist.length+user.blacklist.length)+"</td><td>"+
				user.frames.length+"</td><td>"+
				"<span class='edit-user glyphicon glyphicon-pencil' data-email='"+user.email+"'></span></td></tr>"
			)
		})

		$(".edit-user").click(function(){
			var email = $(this).data("email")
			showUser(email)
		})
	}).fail(function fail() {
		// TODO
		alert("Session expired")
		location.reload();
	})
}

var currentUser = ""
function showUser(email) {
	currentUser = email
	console.log("show")

	$("#dashboard").hide()
	$("#edit-user-page").show()

	$("#edit-user-page-email").html(email)

	$("#whitelist").html("")
	$.each(users[currentUser].whitelist, function(i, em) {
		$("#whitelist").append("<tr><td>"+em+
			"</td><td style='width: 25px'><span class='glyphicon glyphicon-remove remove-whitelist' data-remove='"+
			em+"'></span></td></tr>")
	})

	if(!users[currentUser].whitelist.length) {
		$("#whitelist").append("<tr><td>Whitelist is empty</td></tr>")
	}

	$("#blacklist").html("")
	$.each(users[currentUser].blacklist, function(i, em) {
		$("#blacklist").append("<tr><td>"+em+
			"</td><td style='width: 25px'><span class='glyphicon glyphicon-remove remove-blacklist' data-remove='"+
			em+"'></span></td></tr>")
	})

	if(!users[currentUser].blacklist.length) {
		$("#blacklist").append("<tr><td>Blacklist is empty</td></tr>")
	}

	$(".remove-whitelist").click(function() {
		var emailToRemove = $(this).data("remove")
		$.ajax({
			url: "/api/v1/user/"+currentUser+"/whitelist",
			type: "DELETE",
			data: {email: emailToRemove},
			success: function() {
				var index = users[currentUser].whitelist.indexOf(emailToRemove)
				if (index > -1) {
				    users[currentUser].whitelist.splice(index, 1);
				}
				showUser(currentUser)
			}
	    })
	})

	$(".remove-blacklist").click(function() {
		var emailToRemove = $(this).data("remove")
		$.ajax({
			url: "/api/v1/user/"+currentUser+"/blacklist",
			type: "DELETE",
			data: {email: emailToRemove},
			success: function() {
				var index = users[currentUser].blacklist.indexOf(emailToRemove)
				if (index > -1) {
				    users[currentUser].blacklist.splice(index, 1);
				}
				showUser(currentUser)
			}
	    })
	})

	$.get("/api/v1/user/"+currentUser+"/frames", function(data) {
		$("#frameslist").html("")
		$.each(data, function(i, frame) {
			$("#frameslist").append("<tr><td>"+frame.name+(frame.code? ", "+frame.code : "")+
				(frame.orderNumber? ", order: "+frame.orderNumber : "")+
				(frame.createdAt? ", at "+frame.createdAt : "")+
				"</td><td style='width: 25px'><span class='glyphicon glyphicon-th newcode-frameslist' data-id='"+
				frame._id+"'></span></td><td style='width: 25px'><span class='glyphicon glyphicon-remove remove-frameslist' data-remove='"+
				frame._id+"'></span></td></tr>")
		})

		if(!data.length) {
			$("#frameslist").append("<tr><td style='text-align: center'>This user has no frames<br>"+
				"<button type=button class='btn btn-danger' id=delete-user>DELETE this user</button>"+
				"</td></tr>")

			$("#delete-user").click(function() {
				if(confirm("Are you sure?")){
					$.ajax({
						url: "/api/v1/user/"+currentUser,
						type: "DELETE",
						success: function() {
							showUsers()
						}
				    })
				}
			})
		}

		$(".remove-frameslist").click(function() {
			var idToRemove = $(this).data("remove")
			$.ajax({
				url: "/api/v1/frame/"+idToRemove,
				type: "DELETE",
				success: function() {
					var index = users[currentUser].frames.indexOf(idToRemove)
					if (index > -1) {
					    users[currentUser].frames.splice(index, 1);
					}
					showUser(currentUser)
				}
		    })
		})

		$(".newcode-frameslist").click(function() {
			var frameId = $(this).data("id")
			$.post("/api/v1/frame/"+frameId+"/newcode", function() {
				showUser(currentUser)
			})
		})
	}).fail(function fail() {
		// TODO
		alert("Session expired")
		location.reload();
	})

}

$(function() {
	$("#loginform").show()
	$("#dashboard").hide()
	$("#edit-user-page").hide()


	$('#users-filter').keyup(function () {
        var rex = new RegExp($(this).val(), 'i');
        $('#users-table tr').hide();
        $('#users-table tr').filter(function () {
            return rex.test($(this).text());
        }).show();
    })

	$("#addUserButton").click(function() {
		var email = $("#addUserEmail").val()
		$("#addUserEmail").val("")
		if(email=="") { return }
		$.post("/api/v1/user/"+email, function(user) {
			showUsers()
		})
	})

	$("#login-button").click(function(e) {
		e.preventDefault()
		$.post('/login', {username: $("#inputEmail").val(), password: $("#inputPassword").val()}, function(data) {
			console.log(data)
			if(data.success) {
				showUsers()
			} else {
				// TODO
				alert("Invalid username or password")
			}
		}).fail(function fail() {
			// TODO
			alert("Invalid username or password")
		})
	})

	$("#back-to-users").click(function() {
		showUsers()
	})

	$("#addWhitelistButton").click(function() {
		var emailToadd = $("#addWhitelistEmail").val().toLowerCase()
		$("#addWhitelistEmail").val("")
		if(emailToadd=="") { return }
		if (users[currentUser].whitelist.indexOf(emailToadd)>-1 || 
			users[currentUser].blacklist.indexOf(emailToadd)>-1) {
			alert("This email is already in the list")
			return
		}

		$.post("/api/v1/user/"+currentUser+"/whitelist", {email: emailToadd}, function(data) {
			users[currentUser].whitelist.push(emailToadd)
			showUser(currentUser)
		})
	})

	$("#addBlacklistButton").click(function() {
		var emailToadd = $("#addBlacklistEmail").val().toLowerCase()
		$("#addBlacklistEmail").val("")
		if(emailToadd=="") { return }
		if (users[currentUser].whitelist.indexOf(emailToadd)>-1 || 
			users[currentUser].blacklist.indexOf(emailToadd)>-1) {
			alert("This email is already in the list")
			return
		}

		$.post("/api/v1/user/"+currentUser+"/blacklist", {email: emailToadd}, function(data) {
			users[currentUser].blacklist.push(emailToadd)
			showUser(currentUser)
		})
	})

	$("#addFrameButton").click(function() {
		var name = $("#addFrameName").val().toLowerCase().trim()
		$("#addFrameName").val("")
		if(name=="") { return }

		if(!/^[a-zA-Z0-9\.\-_]*$/.test(name)) {
			alert("Incorrect frame name")
			return
		}
		/*if (users[currentUser].frames.indexOf(emailToadd)>-1) {
			alert("This frame already exists")
			return
		}*/
		$.post("/api/v1/user/"+currentUser+"/frames", {name: name}, function(data) {
			users[currentUser].frames.push(name)
			showUser(currentUser)
		})
	})

	if(sessionUsername!="") {
		showUsers()
	}


})