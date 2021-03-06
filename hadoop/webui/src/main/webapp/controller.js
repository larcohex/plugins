'use strict';

angular.module('subutai.plugins.hadoop.controller', [])
    .controller('HadoopCtrl', HadoopCtrl)
	.directive('colSelectHadoopContainers', colSelectHadoopContainers)
	.directive('checkboxListDropdown', checkboxListDropdown);

HadoopCtrl.$inject = ['hadoopSrv', 'SweetAlert', 'DTOptionsBuilder', 'DTColumnDefBuilder'];

function HadoopCtrl(hadoopSrv, SweetAlert, DTOptionsBuilder, DTColumnDefBuilder) {
    var vm = this;
	vm.activeTab = 'install';
	vm.hadoopInstall = {};
	vm.environments = [];
	vm.containers = [];
	vm.clusters = [];

	//functions
	vm.createHadoop = createHadoop;
	vm.showContainers = showContainers;
	vm.addContainer = addContainer;
	vm.getClustersInfo = getClustersInfo;
	vm.changeClusterScaling = changeClusterScaling;
	vm.deleteCluster = deleteCluster;
	vm.addNode = addNode;
	vm.startNode = startNode;
	vm.stopNode = stopNode;

	setDefaultValues();

	hadoopSrv.getEnvironments().success(function (data) {
		vm.environments = data;
	});

	function getClusters() {
		hadoopSrv.getClusters().success(function (data) {
			vm.clusters = data;
		});
	}
	getClusters();

	function getClustersInfo(selectedCluster) {
		LOADING_SCREEN();
		hadoopSrv.getClusters(selectedCluster).success(function (data) {
			vm.currentCluster = data;
			console.log(vm.currentCluster);
			LOADING_SCREEN('none');
		}).error(function(data) {
			console.log(data);
			LOADING_SCREEN('none');
		});
	}

	function changeClusterScaling(scale) {
		if(vm.currentCluster.clusterName === undefined) return;
		try {
			hadoopSrv.changeClusterScaling(vm.currentCluster.clusterName, scale);
		} catch(e) {}
	}

	function addNode() {
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal("Success!", "Node adding is in progress.", "success");
		hadoopSrv.addNode(vm.currentCluster.clusterName).success(function (data) {
			SweetAlert.swal(
				"Success!",
				"Node has been added to cluster " + vm.currentCluster.clusterName + ".",
				"success"
			);
			getClustersInfo(vm.currentCluster.clusterName);
		});
	}

	function startNode(node, nodeType) {
		if(vm.currentCluster.clusterName === undefined) return;
		node.status = 'STARTING';
		hadoopSrv.startNode(vm.currentCluster.clusterName, nodeType).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes have been started successfully. LOG: " + data.replace(/\\n/g, ' ').substring (0, 40), "success");
			node.status = 'RUNNING';
			//getClustersInfo(vm.currentCluster.name);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Failed to start cluster error: ' + error.replace(/\\n/g, ' '), "error");
			node.status = 'ERROR';
		});
	}

	function stopNode(node, nodeType) {
		if(vm.currentCluster.clusterName === undefined) return;
		node.status = 'STOPPING';
		hadoopSrv.stopNode(vm.currentCluster.clusterName, nodeType).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes have stopped successfully. LOG: " + data.replace(/\\n/g, ' ').substring (0, 40), "success");
			//getClustersInfo(vm.currentCluster.name);
			node.status = 'STOPPED';
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Failed to stop cluster error: ' + error.replace(/\\n/g, ' '), "error");
			node.status = 'ERROR';
		});
	}

	function deleteCluster() {
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal({
			title: "Are you sure?",
			text: "Your will not be able to recover this cluster!",
			type: "warning",
			showCancelButton: true,
			confirmButtonColor: "#ff3f3c",
			confirmButtonText: "Delete",
			cancelButtonText: "Cancel",
			closeOnConfirm: false,
			closeOnCancel: true,
			showLoaderOnConfirm: true
		},
		function (isConfirm) {
			if (isConfirm) {
				hadoopSrv.deleteCluster(vm.currentCluster.clusterName).success(function (data) {
					SweetAlert.swal("Deleted!", "Cluster has been deleted.", "success");
					vm.currentCluster = {};
					getClusters();
				});
			}
		});
	}

	function createHadoop() {
		SweetAlert.swal("Success!", "Hadoop cluster is being created.", "success");
		vm.activeTab = 'manage';
		LOADING_SCREEN();
		hadoopSrv.createHadoop(JSON.stringify(vm.hadoopInstall)).success(function (data) {
			SweetAlert.swal("Success!", "Hadoop cluster creation message:" + data.replace(/\\n/g, ' '), "success");
			getClusters();
			LOADING_SCREEN ("none");
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Hadoop cluster creation error: ' + error.replace(/\\n/g, ' '), "error");
			getClusters();
			LOADING_SCREEN ("none");
		});
		setDefaultValues();
	}

	function showContainers(environmentId) {
		vm.containers = [];
		vm.seeds = [];		
		for(var i in vm.environments) {
			if(environmentId == vm.environments[i].id) {
				for (var j = 0; j < vm.environments[i].containers.length; j++){
					if(vm.environments[i].containers[j].templateName == 'hadoop') {
						vm.containers.push(vm.environments[i].containers[j]);
					}
				}
				break;
			}
		}
	}

	function addContainer(containerId) {
		if(vm.hadoopInstall.slaves.indexOf(containerId) > -1) {
			vm.hadoopInstall.slaves.splice(vm.hadoopInstall.slaves.indexOf(containerId), 1);
		} else {
			vm.hadoopInstall.slaves.push(containerId);
		}
	}	

	vm.dtOptions = DTOptionsBuilder
		.newOptions()
		.withOption('order', [[2, "asc" ]])
		.withOption('stateSave', true)
		.withPaginationType('full_numbers');
	vm.dtColumnDefs = [
		DTColumnDefBuilder.newColumnDef(0).notSortable(),
		DTColumnDefBuilder.newColumnDef(1),
		DTColumnDefBuilder.newColumnDef(2),
		DTColumnDefBuilder.newColumnDef(3).notSortable()
	];

	function setDefaultValues() {
		vm.hadoopInstall = {};
		vm.hadoopInstall.domainName = 'intra.lan';
		vm.hadoopInstall.replicationFactor = 1;
		vm.hadoopInstall.slaves = [];
	}	


	vm.info = {};
	hadoopSrv.getPluginInfo().success (function (data) {
		vm.info = data;
	});
}

function colSelectHadoopContainers() {
	return {
		restrict: 'E',
		templateUrl: 'plugins/hadoop/directives/col-select/col-select-containers.html'
	}
};

function checkboxListDropdown() {
	return {
		restrict: 'A',
		link: function(scope, element, attr) {
			$(".b-form-input_dropdown").click(function () {
				$(this).toggleClass("is-active");
			});

			$(".b-form-input-dropdown-list").click(function(e) {
				e.stopPropagation();
			});
		}
	}
};

