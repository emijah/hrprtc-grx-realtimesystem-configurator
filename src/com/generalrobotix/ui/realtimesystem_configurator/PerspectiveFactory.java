package com.generalrobotix.ui.realtimesystem_configurator;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactory implements IPerspectiveFactory {
	public void createInitialLayout(IPageLayout layout) {
	     // エディタエリアの取得
	     String editorArea = layout.getEditorArea();
	     // エディタエリアを非表示に設定
	     //layout.setEditorAreaVisible(false);
	     
	     // エディタの左上
	     IFolderLayout leftTop = layout.createFolder("leftTop_folder", IPageLayout.LEFT, (float) 0.25f, editorArea);
	     leftTop.addView("jp.go.aist.rtm.nameserviceview.ui.views.nameserviceview.NameServiceView");
	     leftTop.addView("jp.go.aist.rtm.repositoryView.view");
	     
	     IFolderLayout leftBottom = layout.createFolder("leftBottom_folder", IPageLayout.BOTTOM, (float) 0.5f, "jp.go.aist.rtm.repositoryView.view");
	     //leftBottom.addView("org.eclipse.jdt.ui.PackageExplorer");
	     leftBottom.addView("com.generalrobotix.ui.realtimesystem_configurator.benchmarkresult_explorer");
	     
	     // エディタの右側
	     IFolderLayout rightTop = layout.createFolder("rightTop_folder", IPageLayout.RIGHT, (float) 0.7f, editorArea);
	     rightTop.addView(IPageLayout.ID_PROP_SHEET);
	     IFolderLayout rightBottom = layout.createFolder("rightBottom_folder", IPageLayout.BOTTOM, (float) 0.5f, IPageLayout.ID_PROP_SHEET);
	     rightBottom.addView("com.generalrobotix.ui.realtimesystem_configurator.benchmarkoperator");

	     
	     // エディタの下側に・ビューを生成
	     IFolderLayout bottom = layout.createFolder("bottom_folder", IPageLayout.BOTTOM, (float) 0.6f, editorArea);
	     bottom.addView("com.generalrobotix.ui.realtimesystem_configurator.topologyview");
	     bottom.addView("com.generalrobotix.ui.realtimesystem_configurator.timingchartview");
	     bottom.addView("org.eclipse.ui.console.ConsoleView");
	}
}
