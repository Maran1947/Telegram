package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;

import java.util.ArrayList;

public class LinkActionView extends LinearLayout {

    TextView linkView;
    String link;
    BaseFragment fragment;
    ImageView optionsView;
    private final TextView copyView;
    private final TextView shareView;
    private final TextView removeView;
    private final FrameLayout frameLayout;

    private Delegate delegate;

    private ActionBarPopupWindow actionBarPopupWindow;
    private final AvatarsContainer avatarsContainer;
    private int usersCount;

    private boolean revoked;
    private boolean permanent;
    boolean loadingImporters;
    private QRCodeBottomSheet qrCodeBottomSheet;
    private boolean isPublic;

    public LinkActionView(Context context, BaseFragment fragment, BottomSheet bottomSheet, int chatId, boolean permanent) {
        super(context);
        this.fragment = fragment;
        this.permanent = permanent;
        setOrientation(VERTICAL);
        frameLayout = new FrameLayout(context);
        linkView = new TextView(context);
        linkView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(40), AndroidUtilities.dp(18));
        linkView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

        frameLayout.addView(linkView);
        optionsView = new ImageView(context);
        optionsView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ab_other));
        optionsView.setScaleType(ImageView.ScaleType.CENTER);
        frameLayout.addView(optionsView,  LayoutHelper.createFrame(40, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        addView(frameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 4, 0));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(HORIZONTAL);

        copyView = new TextView(context);
        copyView.setGravity(Gravity.CENTER_HORIZONTAL);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_copy_filled)), 0, 1, 0);
        spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(8)), 1, 2, 0);
        spannableStringBuilder.append(LocaleController.getString("CopyLink", R.string.CopyLink));
        spannableStringBuilder.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(5)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
        copyView.setText(spannableStringBuilder);
        copyView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        copyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        copyView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        copyView.setLines(1);
        linearLayout.addView(copyView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 4, 0, 4, 0));

        shareView = new TextView(context);
        shareView.setGravity(Gravity.CENTER_HORIZONTAL);
        spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_share_filled)), 0, 1, 0);
        spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(8)), 1, 2, 0);
        spannableStringBuilder.append(LocaleController.getString("ShareLink", R.string.ShareLink));
        spannableStringBuilder.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(5)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
        shareView.setText(spannableStringBuilder);
        shareView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

        shareView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        shareView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        shareView.setLines(1);
        linearLayout.addView(shareView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 4, 0, 4, 0));


        removeView = new TextView(context);
        removeView.setGravity(Gravity.CENTER_HORIZONTAL);
        spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_delete)), 0, 1, 0);
        spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(8)), 1, 2, 0);
        spannableStringBuilder.append(LocaleController.getString("DeleteLink", R.string.DeleteLink));
        spannableStringBuilder.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(5)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
        removeView.setText(spannableStringBuilder);
        removeView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        removeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        removeView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        removeView.setLines(1);
        linearLayout.addView(removeView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 4, 0, 4, 0));
        removeView.setVisibility(View.GONE);


        addView(linearLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0 , 20, 0, 0));

        avatarsContainer = new AvatarsContainer(context);
        addView(avatarsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 28 + 16, 0 , 12, 0, 0));
        copyView.setOnClickListener(view -> {
            try {
                if (link == null) {
                    return;
                }
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", link);
                clipboard.setPrimaryClip(clip);
                if (bottomSheet != null && bottomSheet.getContainer() != null) {
                    BulletinFactory.createCopyLinkBulletin(bottomSheet.getContainer()).show();
                } else {
                    BulletinFactory.createCopyLinkBulletin(fragment).show();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        if (permanent) {
            avatarsContainer.setOnClickListener(view -> {
                delegate.showUsersForPermanentLink();
            });
        }

        shareView.setOnClickListener(view -> {
            try {
                if (link == null) {
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, link);
                fragment.startActivityForResult(Intent.createChooser(intent, LocaleController.getString("InviteToGroupByLink", R.string.InviteToGroupByLink)), 500);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        removeView.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
            builder.setTitle(LocaleController.getString("DeleteLink", R.string.DeleteLink));
            builder.setMessage(LocaleController.getString("DeleteLinkHelp", R.string.DeleteLinkHelp));
            builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface2, i2) -> {
                if (delegate != null) {
                    delegate.removeLink();
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            fragment.showDialog(builder.create());
        });

        optionsView.setOnClickListener(view -> {
            if (isPublic) {
                showQrCode();
                return;
            }
            if (actionBarPopupWindow != null) {
                return;
            }
            ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context);

            ActionBarMenuSubItem subItem;
            if (!permanent) {
                subItem = new ActionBarMenuSubItem(context, true, false);
                subItem.setTextAndIcon(LocaleController.getString("Edit", R.string.Edit), R.drawable.msg_edit);
                layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                subItem.setOnClickListener(view12 -> {
                    delegate.editLink();
                    if (actionBarPopupWindow != null) {
                        actionBarPopupWindow.dismiss();
                    }
                });
            }

//            subItem = new ActionBarMenuSubItem(context, true, false);
//            subItem.setTextAndIcon(LocaleController.getString("GetQRCode", R.string.GetQRCode), R.drawable.msg_qrcode);
//            layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
//            subItem.setOnClickListener(view12 -> {
//                showQrCode();
//            });

            subItem = new ActionBarMenuSubItem(context, false, true);
            subItem.setTextAndIcon(LocaleController.getString("RevokeLink", R.string.RevokeLink), R.drawable.msg_delete);
            subItem.setColors(Theme.getColor(Theme.key_windowBackgroundWhiteRedText), Theme.getColor(Theme.key_windowBackgroundWhiteRedText));
            subItem.setOnClickListener(view1 -> {
                revokeLink();
                if (actionBarPopupWindow != null) {
                    actionBarPopupWindow.dismiss();
                }
            });
            layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

            FrameLayout container;
            float y = 0;
            if (bottomSheet == null) {
                container = fragment.getParentLayout();
            } else {
                container = bottomSheet.getContainer();
            }

            View v = frameLayout;
            while (v != container) {
                y += v.getY();
                v = (View) v.getParent();
                if (!(v instanceof ViewGroup)) {
                    return;
                }
            }

            if (container != null) {
                FrameLayout finalContainer = container;
                View dimView = new View(context) {
                    @Override
                    protected void onDraw(Canvas canvas) {
                        canvas.drawColor(0x33000000);
                        float x = 0;
                        float y = 0;
                        View v = frameLayout;
                        while (v != finalContainer) {
                            y += v.getY();
                            x += v.getX();
                            v = (View) v.getParent();
                            if (!(v instanceof ViewGroup)) {
                                return;
                            }
                        }
                        canvas.save();
                        canvas.translate(x, y);
                        frameLayout.draw(canvas);
                        canvas.restore();
                    }
                };
                container.addView(dimView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                dimView.setAlpha(0);
                dimView.animate().alpha(1f).setDuration(150);
                layout.measure(MeasureSpec.makeMeasureSpec(container.getMeasuredWidth(), MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(container.getMeasuredHeight(), MeasureSpec.UNSPECIFIED));


                actionBarPopupWindow = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
                    @Override
                    public void dismiss() {
                        super.dismiss();
                        actionBarPopupWindow = null;
                        dimView.animate().cancel();
                        dimView.animate().alpha(0).setDuration(150).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (dimView.getParent() != null) {
                                    finalContainer.removeView(dimView);
                                }
                            }
                        });
                    }
                };
                actionBarPopupWindow.setOutsideTouchable(true);
                actionBarPopupWindow.setClippingEnabled(true);
                actionBarPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
                actionBarPopupWindow.setFocusable(true);
                actionBarPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
                actionBarPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
                actionBarPopupWindow.getContentView().setFocusableInTouchMode(true);

                layout.setDispatchKeyEventListener(keyEvent -> {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && actionBarPopupWindow.isShowing()) {
                        actionBarPopupWindow.dismiss(true);
                    }
                });

                actionBarPopupWindow.showAtLocation(container, 0, (int) container.getMeasuredWidth() - layout.getMeasuredWidth() - AndroidUtilities.dp(16), (int) y + frameLayout.getMeasuredHeight());
            }

        });

        frameLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                copyView.callOnClick();
            }
        });
        updateColors();
    }

    private void showQrCode() {
        qrCodeBottomSheet = new QRCodeBottomSheet(getContext(), link) {
            @Override
            public void dismiss() {
                super.dismiss();
                qrCodeBottomSheet = null;
            }
        };
        qrCodeBottomSheet.show();
        if (actionBarPopupWindow != null) {
            actionBarPopupWindow.dismiss();
        }
    }

    public void updateColors() {
        copyView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        shareView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        removeView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        copyView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        shareView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        removeView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_chat_attachAudioBackground), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        frameLayout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_graySection), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_listSelector), (int) (255 * 0.3f))));
        linkView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        optionsView.setColorFilter(Theme.getColor(Theme.key_dialogTextGray3));
        //optionsView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        avatarsContainer.countTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        avatarsContainer.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6),0,  ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), (int) (255 * 0.3f))));

        if (qrCodeBottomSheet != null) {
            qrCodeBottomSheet.updateColors();
        }
    }


    public void setLink(String link) {
        this.link = link;
        if (link == null) {
            linkView.setText(LocaleController.getString("Loading", R.string.Loading));
        } else if (link.startsWith("https://")) {
            linkView.setText(link.substring("https://".length()));
        } else {
            linkView.setText(link);
        }
    }

    public void setRevoke(boolean revoked) {
        this.revoked = revoked;
        if (revoked) {
            optionsView.setVisibility(View.GONE);
            shareView.setVisibility(View.GONE);
            copyView.setVisibility(View.GONE);
            removeView.setVisibility(View.VISIBLE);
        } else {
            optionsView.setVisibility(View.VISIBLE);
            shareView.setVisibility(View.VISIBLE);
            copyView.setVisibility(View.VISIBLE);
            removeView.setVisibility(View.GONE);
        }
    }

    public void showOptions(boolean b) {
        optionsView.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    public void setPublic(boolean b) {
        if (isPublic != b) {
            isPublic = b;
            if (isPublic) {
                optionsView.setVisibility(View.GONE);
                optionsView.setImageDrawable(ContextCompat.getDrawable(optionsView.getContext(), R.drawable.msg_qrcode));
            } else {
                optionsView.setVisibility(View.VISIBLE);
                optionsView.setImageDrawable(ContextCompat.getDrawable(optionsView.getContext(), R.drawable.ic_ab_other));
            }
        }
    }

    private class AvatarsContainer extends FrameLayout {

        TextView countTextView;
        AvatarsImageView avatarsImageView;

        public AvatarsContainer(@NonNull Context context) {
            super(context);
            avatarsImageView = new AvatarsImageView(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int N = Math.min(3, usersCount);
                    int x = N == 0 ? 0 :(20 * (N - 1) + 24 + 8);
                    super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(x), MeasureSpec.EXACTLY), heightMeasureSpec);
                }
            };

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(HORIZONTAL);

            addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));

            countTextView = new TextView(context);
            countTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            countTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            linearLayout.addView(avatarsImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
            linearLayout.addView(countTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            setPadding(0, AndroidUtilities.dp(8), 0 ,AndroidUtilities.dp(8));
            avatarsImageView.commitTransition(false);
        }
    }

    private void revokeLink() {
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setMessage(LocaleController.getString("RevokeAlert", R.string.RevokeAlert));
        builder.setTitle(LocaleController.getString("RevokeLink", R.string.RevokeLink));
        builder.setPositiveButton(LocaleController.getString("RevokeButton", R.string.RevokeButton), (dialogInterface, i) -> {
            if (delegate != null) {
                delegate.revokeLink();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    public void setUsers(int usersCount, ArrayList<TLRPC.User> importers) {
        this.usersCount = usersCount;
        if (usersCount == 0) {
            avatarsContainer.setVisibility(View.GONE);
            setPadding(AndroidUtilities.dp(19), AndroidUtilities.dp(18), AndroidUtilities.dp(19), AndroidUtilities.dp(18));
        } else {
            avatarsContainer.setVisibility(View.VISIBLE);
            setPadding(AndroidUtilities.dp(19), AndroidUtilities.dp(18), AndroidUtilities.dp(19), AndroidUtilities.dp(10));
            avatarsContainer.countTextView.setText(LocaleController.formatPluralString("PeopleJoined", usersCount));
            avatarsContainer.requestLayout();
        }
        if (importers != null) {
            for (int i = 0; i < 3; i++) {
                if (i < importers.size()) {
                    MessagesController.getInstance(UserConfig.selectedAccount).putUser(importers.get(i), false);
                    avatarsContainer.avatarsImageView.setObject(i, UserConfig.selectedAccount, importers.get(i));
                } else {
                    avatarsContainer.avatarsImageView.setObject(i, UserConfig.selectedAccount, null);
                }
                avatarsContainer.avatarsImageView.commitTransition(false);
            }
        }
    }

    public void loadUsers(TLRPC.TL_chatInviteExported invite, int chatId) {
        if (invite == null) {
            setUsers(0, null);
            return;
        }
        setUsers(invite.usage, invite.importers);
        if (invite.usage > 0 && invite.importers == null && !loadingImporters) {
            TLRPC.TL_messages_getChatInviteImporters req = new TLRPC.TL_messages_getChatInviteImporters();
            req.link = invite.link;
            req.peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(-chatId);
            req.offset_user = new TLRPC.TL_inputUserEmpty();
            req.limit = Math.min(invite.usage, 3);

            loadingImporters = true;
            ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> {
                AndroidUtilities.runOnUIThread(() -> {
                    loadingImporters = false;
                    if (error == null) {
                        TLRPC.TL_messages_chatInviteImporters inviteImporters = (TLRPC.TL_messages_chatInviteImporters) response;
                        if (invite.importers == null) {
                            invite.importers = new ArrayList<>(3);
                        }
                        invite.importers.clear();
                        for (int i = 0; i < inviteImporters.users.size(); i++) {
                            invite.importers.addAll(inviteImporters.users);
                        }
                        setUsers(invite.usage, invite.importers);
                    }
                });
            });
        }
    }

    public interface Delegate {
        void revokeLink();
        default void editLink() {}
        default void removeLink() {}
        default void showUsersForPermanentLink() {}
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }
}