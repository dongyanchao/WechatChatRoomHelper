package com.zdy.project.wechat_chatroom_helper.plugins

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.gh0u1l5.wechatmagician.spellbook.interfaces.IActivityHook
import com.zdy.project.wechat_chatroom_helper.Constants
import com.zdy.project.wechat_chatroom_helper.PageType
import com.zdy.project.wechat_chatroom_helper.wechat.chatroomView.ChatRoomViewPresenter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.hookAllConstructors

@SuppressLint("StaticFieldLeak")
/**
 * Created by Mr.Zdy on 2018/4/1.
 */
object MainLauncherUI : IActivityHook {

    private lateinit var mActivity: Activity


    fun execute() {

        hookAllConstructors(PluginEntry.classloader.loadClass(Constants.FitSystemWindowLayoutView), object : XC_MethodHook() {

            override fun afterHookedMethod(param: MethodHookParam) {


                super.afterHookedMethod(param)


                val fitSystemWindowLayoutView = param.thisObject as ViewGroup
                fitSystemWindowLayoutView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View, child: View) {

                        val chattingView: View//聊天View
                        val chattingViewPosition: Int//聊天View的下標
                        var fitWindowChildCount = 0//fitSystemWindowLayoutView的 child 数量
                        var chatRoomViewPosition = 0
                        var officialViewPosition = 0

                        /*
                         * 微信在某个版本之后 View 数量发生变化，下标也要相应刷新
                         **/
                        if (isWechatHighVersion(1140)) {
                            fitWindowChildCount = 3
                            chattingViewPosition = 2
                            chatRoomViewPosition = 2
                            officialViewPosition = 3
                        } else {
                            fitWindowChildCount = 2
                            chattingViewPosition = 1
                            chatRoomViewPosition = 1
                            officialViewPosition = 2
                        }


                        if (fitSystemWindowLayoutView.childCount != fitWindowChildCount) return
                        if (fitSystemWindowLayoutView.getChildAt(0) !is LinearLayout) return

                        chattingView = fitSystemWindowLayoutView.getChildAt(chattingViewPosition)

                        if (chattingView.javaClass.simpleName != "TestTimeForChatting") return

                        onFitSystemWindowLayoutViewReady(chatRoomViewPosition, officialViewPosition, fitSystemWindowLayoutView)
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {}
                })
            }
        })

    }

    /**
     * 微信自1140之後，fitSystemWindowLayoutView 的 child 數量有所變化
     */
    private fun isWechatHighVersion(wechatVersion: Int): Boolean {
        return wechatVersion >= 1140
    }

    override fun onActivityCreating(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityCreating(activity, savedInstanceState)
        if (activity::class.java.simpleName != "LauncherUI") return

        mActivity = activity

        PluginEntry.chatRoomViewPresenter = ChatRoomViewPresenter(mActivity, PageType.CHAT_ROOMS)
        PluginEntry.officialViewPresenter = ChatRoomViewPresenter(mActivity, PageType.OFFICIAL)
    }


    fun onFitSystemWindowLayoutViewReady(chatRoomIndex: Int, officialIndex: Int, fitSystemWindowLayoutView: ViewGroup) {

        val chatRoomViewParent = PluginEntry.chatRoomViewPresenter.presenterView.parent
        if (chatRoomViewParent != null) {
            (chatRoomViewParent as ViewGroup).removeView(PluginEntry.chatRoomViewPresenter.presenterView)
        }

        val officialViewParent = PluginEntry.officialViewPresenter.presenterView.parent
        if (officialViewParent != null) {
            (chatRoomViewParent as ViewGroup).removeView(PluginEntry.officialViewPresenter.presenterView)
        }

        fitSystemWindowLayoutView.addView(PluginEntry.chatRoomViewPresenter.presenterView, chatRoomIndex)
        fitSystemWindowLayoutView.addView(PluginEntry.officialViewPresenter.presenterView, officialIndex)


        if ((fitSystemWindowLayoutView.getChildAt(0) as ViewGroup).childCount != 2)
            return
        val mainView = (fitSystemWindowLayoutView.getChildAt(0) as ViewGroup).getChildAt(1)


        //调整布局大小，解决部分虛擬按鍵手機的問題
        mainView.viewTreeObserver
                .addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener {
                    val left = mainView.left
                    val right = mainView.right
                    val top = mainView.top
                    val bottom = mainView.bottom

                    val width = right - left
                    val height = bottom - top

                    if (width == 0 || height == 0) return@OnGlobalLayoutListener

                    val chatRoomViewPresenterPresenterView = PluginEntry.chatRoomViewPresenter.presenterView
                    val officialViewPresenterPresenterView = PluginEntry.officialViewPresenter.presenterView


                    val left1 = chatRoomViewPresenterPresenterView.left
                    val top1 = chatRoomViewPresenterPresenterView.top
                    val right1 = chatRoomViewPresenterPresenterView.right
                    val bottom1 = chatRoomViewPresenterPresenterView.bottom

                    if (Rect(left1, top1, right1, bottom1) == Rect(left, top, right, bottom))
                        return@OnGlobalLayoutListener

                    val params = FrameLayout.LayoutParams(width, height)
                    params.setMargins(0, top, 0, 0)

                    chatRoomViewPresenterPresenterView.layoutParams = params
                    officialViewPresenterPresenterView.layoutParams = params
                })
    }


}