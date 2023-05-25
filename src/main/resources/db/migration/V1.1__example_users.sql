insert into users(id, name, password_hash)
select '4e37a586-fb27-11ed-be56-0242ac120002', 'user1', '0b14d501a594442a01c6859541bcb3e8164d183d32937b851835442f69d5c94e' /*password1*/ union all
select '4e37a950-fb27-11ed-be56-0242ac120002', 'user2', '6cf615d5bcaac778352a8f1f3360d23f02f34ec182e259897fd6ce485d7870d4' /*password2*/;
